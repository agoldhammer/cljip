(ns agold.cljip
  (:require [config.core :as e]
            [clojure.string :as string]
            [cheshire.core :as ch]
            [org.httpkit.client :as http])
  (:import [java.net InetAddress])
  (:gen-class))


;; using app.ipgeolocation.io for ip lookup
"
 # Get geolocation for an IPv4 IP Address = 8.8.8.8
$ curl 'https://api.ipgeolocation.io/ipgeo?apiKey=API_KEY&ip=8.8.8.8'

# Get geolocation for an IPv6 IP Address = 2001:4860:4860::1
$ curl 'https://api.ipgeolocation.io/ipgeo?apiKey=API_KEY&ip=2001:4860:4860::1'

# Get geolocation for a domain name = google.com
$ curl 'https://api.ipgeolocation.io/ipgeo?apiKey=API_KEY&ip=dns.google.com
 
 for fields selection:
 $ curl 'https://api.ipgeolocation.io/ipgeo?apiKey=API_KEY&ip=1.1.1.1&fields=city'
"
(defn get-config
  "load config from config.edn in classpath"
  []
  (e/load-env))

(defonce ipgeo "https://api.ipgeolocation.io/ipgeo")
(defonce API-KEY (:API-KEY (get-config)))

(defn reverse-dns-lookup
  "homespun version of reverse dns"
  [ip]
  (.getCanonicalHostName (InetAddress/getByName ip)))

(defn get-hostname
  "get host from ip address, handle not found exception"
  [ip]
  (let [hostname (atom "none")]
    (try
      (reset! hostname (#(reverse-dns-lookup ip)))
      (catch Exception _ (#(reset! hostname "Host Not Found"))))
    {:hostname @hostname}))

(def get-hostname-cached (memoize get-hostname))

(defn get-site-data
  "fetch reverse dns and geodata for ip addr"
  [ip]
  (let [url (str ipgeo "?apiKey=" API-KEY "&ip=" ip "&fields=geo")]
    (println url)
    #_:clj-kondo/ignore
    (let [resp (http/get url)]
      (doseq [item
              (ch/parse-string (:body @resp) true)]
        (println item)))
    )
  )

(defn greet
  "Callable entry point to the application."
  [data]
  (println "conf key is: " (:API-KEY (get-config)))
  (println "foo is" (string/lower-case "Foo"))
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

#_:clj-kondo/ignore
(comment
(get-hostname-cached "100.35.79.95")
  ;; pool-....
  (get-hostname-cached "190.35.79.95")
  ;; host not found, will return numerical ip addr as string
  (get-hostname-cached "71.192.181.208")
  (greet {:name "Art"})
  (string/lower-case "FOO")
  (get-site-data "8.8.8.8")
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (xyz 1)
  #_(config.core/load-env)
  #_(e/load-env)
  ;; https://github.com/apribase/clj-dns/blob/master/src/clj_dns/core.clj
  ;; https://gist.github.com/mwchambers/1316080
  ;; clojure-interop/java.net
  (.getCanonicalHostName (InetAddress/getByName "100.35.79.95"))
  (.getCanonicalHostName (InetAddress/getByName "8.8.8.8"))
  ;; (jn/InetAddress.get-host-address "8.8.8.8")
  
  )
