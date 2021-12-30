(ns agold.cljip
  (:require [clojure.core.async :as a]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as string]
            [cheshire.core :as ch]
            [org.httpkit.client :as http]
            [agold.dateparser :as dp]
            [agold.ipgeo :as ipg]
            [agold.ip-pp :as ipp])
  (:gen-class))

(defonce ipgeo "https://api.ipgeolocation.io/ipgeo")


(defn get-site-data
  "fetch reverse dns and geodata for ip addr"
  [ip]
  (let [url (str ipgeo "?apiKey=" ipg/API-KEY "&ip=" ip "&fields=geo")]
    #_:clj-kondo/ignore
    (let [resp @(http/get url)]
      (if (= (:status resp) 200)
        {:site-data (dissoc (ch/parse-string (:body resp) true) :ip)}
        {:site-data "N/A"}))))

(def get-site-data-cached (memoize get-site-data))

(defn log->vec-of-lines
  "log file to vector of lines"
  [fname]
  (let [lines []]
    (with-open [rdr (io/reader fname)]
      (into lines (line-seq rdr)))))

(defn parse-line
  "parse line of log"
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
    #_(println date)
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

(defn add-site-data
  "add site data to log entry"
  [le]
  (merge le (get-site-data-cached (:ip le))))

(defn add-site-data-async
  "add site data asynchronously"
  [le result]
  (a/go
    (a/>! result (add-site-data le))
     ;; TODO removing below line produces only first log entry in seq, why??
    (a/close! result)))

(defn add-hostname-async
  "add hostname asynchronously"
  [le result]
  (let [hn-chan (a/chan 20)]
    (a/go
      #_(a/>! result (merge le (ipg/async-get-hostname (:ip le) hn-chan 20)))
      (a/>! result (merge le {:hostname "dummy"}))
     ;; TODO removing below line produces only first log entry in seq, why??
      (a/close! result))))

(defn add-data-async
  "add data to log entries on input channel inch, pass to outch"
  [inch midch outch]

  (a/pipeline-async 8 midch add-hostname-async inch)
  (a/pipeline-async 8 outch add-site-data-async midch)
  #_(a/pipeline-async 8 outch add-site-data-async inch)
  )

(defn process-log
  "process log file"
  [logfname]
  (let [vec-of-les (parse-log logfname)
        outch (ipp/start-print-loop)
        midch (a/chan 2048)
        inch (a/chan 2048)]
    (add-data-async inch midch outch)
    #_(a/go-loop [item (a/<! outch)]
        (when item
          (println item)
          (recur (a/<! outch))))
    (a/go (doseq [le vec-of-les]
            #_(println "adding ...")
            (a/>! inch le)))
    :done))

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
  (greet {:name "Art"})
  (string/lower-case "FOO")
  (get-site-data "8.8.8.8")
  (get-site-data "71.192.181.208")
  ;; https://github.com/apribase/clj-dns/blob/master/src/clj_dns/core.clj
  ;; https://gist.github.com/mwchambers/1316080
  (.getCanonicalHostName (InetAddress/getByName "100.35.79.95"))
  (ipg/get-hostname-cached "8.8.8.8")
  (ipg/get-hostname-cached "47.241.66.187")
  ;; (jn/InetAddress.get-host-address "8.8.8.8")
  (re-find #"\".+" "Hello \"Dolly\"")
  (def logstr "180.95.238.249 - - [27/Feb/2021:01:04:43 +0000] \"GET http://www.soso.com/ HTTP/1.1\" 200 396 \"-\" \"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36\"")
  (re-find #"(\S+).+[\[](\S+).+?\"(.+?)\"" logstr)
  (System/getProperty "user.home")
  (log->vec-of-lines "/home/agold/Prog/cljip/testdata/default.log")
  (println (parse-line logstr))
  (time
   (map parse-line (log->vec-of-lines "/home/agold/Prog/cljip/testdata/default.log")))
  (time (parse-log "/home/agold/Prog/cljip/testdata/default.log"))
  (def default-log "/home/agold/Prog/cljip/testdata/default.log")
  (map add-hostname (parse-log default-log))
  (let [procd (map add-site-data (parse-log default-log))]
    (doseq [le procd]
      (pp/pprint le)))
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
  (-main ""))





