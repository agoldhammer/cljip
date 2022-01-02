(ns agold.cljip
  (:require [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            #_[clojure.string :as string]
            [cheshire.core :as ch]
            [org.httpkit.client :as http]
            [agold.dateparser :as dp]
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

#_(defn add-hostname
    "adds hostname to log entry"
    [le ch]
    (merge le (ipg/async-get-hostname (:ip le) midch 20)))

#_(defn get-site-data
    "fetch reverse dns and geodata for ip addr and put on result-chan"
    [ip result-chan]
    (let [url (str ipgeo "?apiKey=" ipg/API-KEY "&ip=" ip "&fields=geo")]
      #_:clj-kondo/ignore
      (println "debug get-site data for ip" ip)
      (a/go (a/put! result-chan (http/get url)))))

(defn get-site-data-async
  "add site data asynchronously"
  [ip result-chan]
  (let [url (str ipgeo "?apiKey=" ipg/API-KEY "&ip=" ip "&fields=geo")]
    #_:clj-kondo/ignore
    (a/go
      (a/>! result-chan {:ip ip :site-data (http/get url)})
   ;; will need this for decoding
   ;; {:site-data (dissoc (ch/parse-string (:body resp) true) :ip)}
     ;; TODO removing below line produces only first log entry in seq, why??
      (a/close! result-chan))))

#_(defn add-hostname-async
    "add hostname asynchronously"
    [le result]
    (let [hn-chan (a/chan 20)]
      (a/go
     ;; get hostname with max 20 ms delay
        (ipg/async-get-hostname (:ip le) hn-chan 20)
        (a/>! result (merge le (a/<! hn-chan)))
        #_(a/>! result (merge le {:hostname "dummy"}))
     ;; TODO removing below line produces only first log entry in seq, why??
        (a/close! result))))

#_(defn add-data-async
    "add data to log entries on input channel inch, pass to outch"
    [inch midch outch]

    (a/pipeline-async 8 midch add-hostname-async inch)
    (a/pipeline-async 8 outch add-site-data-async midch)
    #_(a/pipeline-async 8 outch add-site-data-async inch))

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
  "data is of form {:ip ip-as-string :site-data pending-http-response}"
  [data reduced-log]
  (a/go
    (let [resp @(:site-data data)
          status (:status resp)
          ip (:ip data)
          body (:body resp)
          mapped-body (ch/parse-string body true)]

      #_(pp/pprint resp)
      (println "processing ip " ip)
      (println mapped-body)
      (if (= status 200)
        (swap! reduced-log assoc-in [ip :site-data] (dissoc mapped-body :ip))
        (swap! reduced-log assoc-in [ip :site-data] "missing")))))

(defn process-log
  "process log file"
  [logfname]
  (let [reduced-log (atom (reduce-log logfname))
        ips (keys @reduced-log)
        key-chan (a/to-chan! ips)
        resp-chan (a/chan 2048)]
    (a/pipeline-async 8 resp-chan get-site-data-async key-chan)
    (println "Processing " logfname (count ips) "ip addresses")
    (ipp/apply-to-channel #(process-site-data % reduced-log) resp-chan)
    (a/<!! ipp/exit-chan)
  (pp/pprint reduced-log)
    :done))

(comment
  (assoc-in {"abc" {:events []}} ["abc" :site-data] {:x 1 :y 2})
  (parse-log "testdata/newer.log")
  (reduce-log "testdata/newer.log")
  (process-log "testdata/newer.log"))

#_:clj-kondo/ignore
(defn proclog
  "Callable entry point to the application."
  [data]
  (println "conf key is: " (:API-KEY (ipg/get-config)))
  (println "Starting, exit after 10 secs")
  (process-log "testdata/newer.log")
  (a/<!! (a/timeout 10000))
  (println "exiting")
  (println (str "Bye, " (or (:file data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println (str "Starting:" (or args "No args")))
  (process-log "testdata/newer.log")
  (println "exiting"))

#_:clj-kondo/ignore
(comment
  (get-site-data "8.8.8.8")
  (get-site-data "71.192.181.208")
  ;; https://github.com/apribase/clj-dns/blob/master/src/clj_dns/core.clj
  ;; https://gist.github.com/mwchambers/1316080
  (.getCanonicalHostName (InetAddress/getByName "100.35.79.95"))
  (def logstr "180.95.238.249 - - [27/Feb/2021:01:04:43 +0000] \"GET http://www.soso.com/ HTTP/1.1\" 200 396 \"-\" \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36\"")
  (re-find #"(\S+).+[\[](\S+).+?\"(.+?)\"" logstr)
  (System/getProperty "user.home")
  (time (parse-log "/home/agold/Prog/cljip/testdata/default.log"))
  (def default-log "/home/agold/Prog/cljip/testdata/default.log")
  (map add-hostname (parse-log default-log))
  (process-log default-log)
  (process-log "testdata/newer.log")
  (def p-ch (a/chan))
  (a/go (println (a/<! p-ch)))
  (a/go (a/>! "Hello"))
  (a/go-loop [item (a/<! p-ch)]
    (when item
      (println item)
      (recur (a/<! p-ch))))
  (a/>!! p-ch "Hello Dolly")
  (println (slurp "config.edn"))
  ;; gets current working directory
  (.getCanonicalFile (clojure.java.io/file "."))

;;; to test le-reducer
  #_(defn test-le-reducer []
      (let [inch (a/chan 1024)]
        (a/go
          (doseq [log-entry (parse-log "testdata/newer.log")]
            (a/>! inch log-entry)))
        (a/reduce ipp/le-reducer {} inch)))
  #_(def out (test-le-reducer))
  (def in (a/chan 1024))
  (def out (a/reduce ipp/le-reducer {} in))
  #_(doseq [log-entry (parse-log "testdata/newer.log")]
      (a/go (a/>! in log-entry)))
  (a/onto-chan!! in (parse-log "testdata/newer.log") false)
  (.count (.buf in))
  (let [in (a/to-chan! (parse-log "testdata/newer.log"))]
    (println "loaded items" (.count (.buf in)))
    (let [out (a/reduce ipp/le-reducer {} in)]
      (a/close! in)
      (println "out items" (.count (.buf out)))
      (a/go-loop [item (a/<! out)]
        (when item
          (println item)
          (recur (a/<! out))))))
  (let [out (a/to-chan! (parse-log "testdata/newer.log"))
        reducible (a/<!! out)]
    #_(reduce ipp/le-reducer {} reducible)
    (println reducible))
  (def reduced-les (reduce ipp/le-reducer {} (parse-log "testdata/newer.log")))
  reduced-les
  (keys reduced-les)
  (count (keys reduced-les))
  (a/<!! in)
  (.count (.buf out))
  (a/<!! out)
  ;; without channel works properly
  (reduce ipp/le-reducer {} (parse-log "testdata/newer.log"))
  (def c (a/chan 1000))
  (def out (a/into [] (a/to-chan! (parse-log "testdata/newer.log"))))
  (a/close! c)
  (.count (.buf out))
  (a/<!! out)
  (a/onto-chan! c (range 5) false)
  (.count (.buf c))
  (a/<!! (a/reduce + 0 (a/onto-chan! c (range 5) false)))
  (use 'clojure.tools.trace))






