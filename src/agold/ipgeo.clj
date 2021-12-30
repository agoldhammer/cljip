(ns agold.ipgeo
  (:require [clojure.core.async :as a]
            [config.core :as e])
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

(defn async-get-hostname
  "get hostname asynchronously and put to result channel
   with timeout max-delay in ms (default 100)"
  ([ip resch]
   (async-get-hostname ip resch 100))
  ([ip resch max-delay]
   (println "agh called")
   (let [hnch (a/chan 1)
         toch (a/timeout max-delay)]
     (a/go
       (a/>! hnch (get-hostname-cached ip))
       (a/alt!
         toch (a/>! resch {:hostname "Timed out"})
         hnch ([hn] (a/>! resch hn)))))))

;; TODO: for testing, remove later
(defn try-async
  [ip delay]
  (let [ch (a/chan 50)]
    (async-get-hostname ip ch delay)
    ch))

(comment
  (reverse-dns-lookup "8.8.8.8")
  (reverse-dns-lookup "47.241.66.187")
  (get-hostname "47.241.66.187")
  (get-hostname-cached "47.241.66.187")
  (get-hostname-cached "100.35.79.95")
  (get-hostname-cached "8.8.8.8")
  ;; pool-....
  (get-hostname-cached "190.35.79.95")
  ;; host not found, will return numerical ip addr as string
  (get-hostname-cached "71.192.181.208")
  (def try (try-async "106.36.79.95" 20))

  (.count (.buf try))
(println (a/<!! try))
  )