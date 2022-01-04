(ns agold.cljip
  (:require [clojure.core.async :as a]
            [clojure.java.io :as io]
            [cheshire.core :as ch]
            [org.httpkit.client :as http]
            [agold.dateparser :as dp]
            [agold.rdns :as rd]
            [agold.ipgeo :as ipg]
            [agold.ip-pp :as ipp])
  (:gen-class))

(defonce ipgeo "https://api.ipgeolocation.io/ipgeo")

(defn log->vec-of-lines
  "log file to vector of lines"
  [fname]
  (let [lines []]
    (with-open [rdr (io/reader fname)]
      (into lines (line-seq rdr)))))

(defn parse-line
  "parse line of log, returns basic log entry
   w/o augmented data"
  [line]
  (let [parse-re #"(\S+).+?[\[](\S+).+?\"(.+?)\""
        parsed (re-find parse-re line)]
    {:entry (parsed 0)
     :ip (parsed 1)
     :date (parsed 2)
     :req (parsed 3)}))

;; log-entry has shape {:entry x :ip y :date d :req r}

(defn fix-date
  "get date part of logentry"
  [log-entry]
  (let [date (:date log-entry)]
    (assoc log-entry :date (dp/datestr->jtime date))))

(defn parse-log
  "logfname to vec of logentries with jdatetimes"
  [logfname]
  (let [vec-logentries (mapv parse-line (log->vec-of-lines logfname))]
    (mapv fix-date vec-logentries)))


(defn get-site-data-async
  "add site data asynchronously, af fn for pipeline"
  [ip result-chan]
  (let [url (str ipgeo "?apiKey=" ipg/API-KEY "&ip=" ip "&fields=geo")]
    #_:clj-kondo/ignore
    (a/go
      (a/>! result-chan {:ip ip :site-data (http/get url)})
   ;; will need this for decoding
   ;; {:site-data (dissoc (ch/parse-string (:body resp) true) :ip)}
     ;; TODO removing below line produces only first log entry in seq, why??
      (a/close! result-chan))))


;; reduce-log yields reduced log, which looks is a map of
;; log entries by ip. Looks like this:
;; {"5.188.210.227"
;;    {events: vec-of events}
;;    eventually to be added: {:site data {...}}
;; vec-of-events looks like:
;; [{:entry "5.188.210.227 - - [30/Dec/2021:05:01:16 +0000] \"\\x05\\x01\\x00\"",
;;   :date #object[java.time.ZonedDateTime 0x6f1e14fd "2021-12-30T05:01:16Z[GMT]"],
;;   :req "\\x05\\x01\\x00"} ... ]
;;   next-ip [list of parsed entries]}

(defn reduce-log
  "convert logfile to map of log entries by ip"
  [logfname]
  (reduce ipp/le-reducer {} (parse-log logfname)))

(defn process-site-data
  "data is of form {:ip ip-as-string :hostname hname
   :site-data pending-http-response}"
  [data reduced-log]
  (a/go
    (let [resp @(:site-data data)
          status (:status resp)
          ip (:ip data)
          body (:body resp)
          mapped-body (ch/parse-string body true)]
      (if (= status 200)
        (swap! reduced-log assoc-in [ip :site-data] (dissoc mapped-body :ip))
        (swap! reduced-log assoc-in [ip :site-data] "missing")))))

(defn process-log
  "process log file"
  [logfname]
  (let [reduced-log (atom (reduce-log logfname))
        ips (keys @reduced-log)
        key-chan (a/to-chan! ips)
        resp-chan (a/chan 2048)
        dns-chan (a/chan 2048)
        ;; thread pool for rev dns lookups
        #_#_thread-pool (rd/make-thread-pool 5)]
    ;;;

    ;;;
    (a/pipeline-async 8 resp-chan get-site-data-async key-chan)
    (println "Processing " logfname (count ips) "ip addresses")
    (ipp/apply-to-channel #(process-site-data % reduced-log) resp-chan)
    ;; TODO for testing, remove later
    #_(doseq [ip ips]
        (rd/async-wrapper thread-pool dns-chan rd/reverse-dns-lookup ip))

    (loop [exit? (a/poll! ipp/exit-chan)]
      (println "exit flag " exit?)
      (when (not exit?)
        (println "waiting on exit")
        (a/<!! (a/timeout 500))
        (recur (a/poll! ipp/exit-chan))))
    (ipp/pp-reduced-log @reduced-log)
    #_(rd/rdns-with-timeout)
    (println "doing host lookups")
    (a/pipeline-async 4 dns-chan #(rd/rdns-with-timeout %1 10 %2) (a/to-chan! ips))
    (println "waiting on host lookups")
    (doseq [host (a/<!! (a/into [] dns-chan))]
      (println host))

    #_(a/go-loop [item (a/<! dns-chan)]
        (when item
          (println item)
          (recur (a/<! dns-chan))))
    #_(.shutdown thread-pool)
  :done))

(comment
  
  (assoc-in {"abc" {:events []}} ["abc" :site-data] {:x 1 :y 2})
  (parse-log "testdata/newer.log")
  (reduce-log "testdata/newer.log")
  (process-log "testdata/short.log")
  (process-log "testdata/acc2022-04-01.log")
  (process-log "testdata/newer.log")
  (time (process-log "testdata/newer.log"))
  (a/poll! ipp/exit-chan))

#_(comment
  (let [dns-chan (a/chan 2048)
        thread-pool (rd/make-thread-pool 5)
        ips ["71.192.181.208" "180.95.231.214" "175.184.164.215"]]
    (doseq [ip ips]
      (println "adding " ip)
      #_(rd/async-wrapper thread-pool dns-chan rd/reverse-dns-lookup ip)
      #_(rd/async-wrapper thread-pool dns-chan (fn [ip] (str "ip is: " ip)) ip))
    (a/go-loop [item (a/<! dns-chan)]
      (when item
        (println item)
        (Thread/sleep 1000)
        (recur (a/<! dns-chan))))
    (Thread/sleep 5000)
    (.shutdown thread-pool))
  
  (def ips (keys (reduce-log "testdata/short.log")))
  (count ips)
  (pmap rd/reverse-dns-lookup ips)

  (map  rd/reverse-dns-lookup ["71.192.181.208" "180.95.231.214" "175.184.164.215"])
  (time (rd/reverse-dns-lookup "71.192.181.208"))
  (time (rd/reverse-dns-lookup "180.95.231.214")))

#_:clj-kondo/ignore
(defn proclog
  "Callable entry point to the application."
[data]
  (println "conf key is: " (:API-KEY (ipg/get-config)))
  (println "Starting, exit after 10 secs")
  (process-log "testdata/newer.log")
  (println "exiting")
  (println (str "Bye, " (or (:file data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (str "Starting:" (or args "No args")))
  (process-log "testdata/small.log")
  (println "exiting"))

#_:clj-kondo/ignore
(comment
  (get-site-data "8.8.8.8")
  (get-site-data "71.192.181.208")

  (def logstr "180.95.238.249 - - [27/Feb/2021:01:04:43 +0000] \"GET http://www.soso.com/ HTTP/1.1\" 200 396 \"-\" \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36\"")
  (re-find #"(\S+).+[\[](\S+).+?\"(.+?)\"" logstr)
  (System/getProperty "user.home")
  (time (parse-log "/home/agold/Prog/cljip/testdata/default.log"))
  (def default-log "/home/agold/Prog/cljip/testdata/default.log")
  ;; gets current working directory
  (.getCanonicalFile (clojure.java.io/file "."))


  (use 'clojure.tools.trace))






