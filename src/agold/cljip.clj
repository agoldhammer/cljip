(ns agold.cljip
  (:require [config.core :as e]
            [clojure.string :as str]
            [org.httpkit.client :as http])
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



(defn get-site-data
  "fetch reverse dns and geodata for ip addr"
  [ip]
  (let [url (str ipgeo "?apiKey=" API-KEY "&ip=" ip "&fields=geo")]
    (println url)
    (let [resp (http/get url)]
      (println (:body @resp)))
    )
  )

(defn greet
  "Callable entry point to the application."
  [data]
  (println "conf key is: " (:API-KEY (get-config)))
  (println "foo is" (str/lower-case "Foo"))
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

(comment
  (greet {:name "Art"})
  (str/lower-case "FOO")
  (get-site-data "2601:184:4081:1c8c:117d:2f0a:ee6f:b860")
  #_(config.core/load-env)
  #_(e/load-env)
  )
