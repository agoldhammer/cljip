(ns agold.cljip
  (:require #_[config.core :as e]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cheshire.core :as ch]
            [org.httpkit.client :as http]
            [agold.dateparser :as dp]
            [agold.ipgeo :as ipg])
  (:gen-class))

(defonce ipgeo "https://api.ipgeolocation.io/ipgeo")


(defn get-site-data
  "fetch reverse dns and geodata for ip addr"
  [ip]
  (let [url (str ipgeo "?apiKey=" ipg/API-KEY "&ip=" ip "&fields=geo")]
    (println url)
    #_:clj-kondo/ignore
    (let [resp (http/get url)]
      (doseq [item
              (ch/parse-string (:body @resp) true)]
        (println item)))
    )
  )

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

#_(defn greet
  "Callable entry point to the application."
  [data]
  (println "conf key is: " (:API-KEY (ipg/get-config)))
  (println "foo is" (string/lower-case "Foo"))
  (println (str "Hello, " (or (:name data) "World") "!")))

#_(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

#_:clj-kondo/ignore
(comment
  (greet {:name "Art"})
  (string/lower-case "FOO")
  (get-site-data "8.8.8.8")
  (get-site-data "71.192.181.208")
  ;; https://github.com/apribase/clj-dns/blob/master/src/clj_dns/core.clj
  ;; https://gist.github.com/mwchambers/1316080
  (.getCanonicalHostName (InetAddress/getByName "100.35.79.95"))
  (.getCanonicalHostName (InetAddress/getByName "8.8.8.8"))
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
  )
