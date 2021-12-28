(ns agold.ipgeo
  (:require [config.core :as e])
  (:import [java.net InetAddress]))

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

(comment
  (get-hostname-cached "100.35.79.95")
  ;; pool-....
  (get-hostname-cached "190.35.79.95")
  ;; host not found, will return numerical ip addr as string
  (get-hostname-cached "71.192.181.208"))