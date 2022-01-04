#_:clj-kondo/ignore
(ns agold.rdns
  (:require [clojure.core.async :refer [put! close! thread] :as a])
  (:import [java.util.concurrent Executors TimeUnit]
           [java.net InetAddress]))

(defn make-thread-pool
  "make pool of nthreads"
  [nthreads]
  (Executors/newFixedThreadPool nthreads))

#_:clj-kondo/ignore
(defn reverse-dns-lookup
  "homespun version of reverse dns"
  [ip]
  {:ip ip :hostname (.getCanonicalHostName (InetAddress/getByName ip))})


(defn ips->hosts
  "translate vec of ips to vec of maps {:ip ip :hostname hn}
   with timeout interval"
  [vec-of-ips nthreads timeout]
  (let [pool (make-thread-pool nthreads)
        tasks (map (fn [ip]
                     (fn []
                       (reverse-dns-lookup ip)))
                   vec-of-ips)]
    (doseq [fut (.invokeAll pool tasks)]
      (try
        (println (.get fut timeout TimeUnit/MILLISECONDS))
        (catch Exception e (ex-info "wrapper err" {:ip "unk"} e))))))


(defn rdns-with-timeout
  "reverse dns lookup of ip with timeout t in ms
   output to channel c"
  [ip t c]
  (let [res (a/chan 1)
        timeout-chan (a/timeout t)]
    (println "looking up host " ip "with t/o" t)
    (a/go
     (a/>! res (reverse-dns-lookup ip))
     (a/alt!!
       timeout-chan (a/>! c {:ip ip :hostname "timed out"})
       res ([v] (a/>! c v))))))

#_(defn get-rdns-async
  "reverse dns with timeout for use in pipeline"
  [ip result-ch]
  (rdns-with-timeout ip 10 result-ch))

#_(a/<!! (thread (reverse-dns-lookup "207.244.248.240")))

#_(defn async-wrapper
  "execute blocking fn f in a thread pool and put results on channel outch"
  [pool outch f & args]
  (.submit pool (fn []
                  (try (put! outch (apply f args))
                       (catch Exception e (put! outch (ex-info "wrapper err" {:args args} e)))
                       (finally (close! outch))))))



(comment 
  (let [outch (a/chan 10)]
    (rdns-with-timeout "46.73.159.203" 5 outch)
    (println (a/<!! outch)))
  #_(def pool (make-thread-pool 5))
  #_(def dnsch (a/chan 10))
  #_(async-wrapper pool dnsch reverse-dns-lookup "35.233.62.116")
  #_(a/go (println (a/<! dnsch)))
  )

#_(defn rdns-fetch
    "do reverse dns lookup on multiple threads"
    [ips]
    (let [nitems (count ips)]))
(.convert TimeUnit/MILLISECONDS 30)
(time
   (let [ips ["39.104.69.228" "71.192.181.208" "124.88.55.27" "180.95.238.249"
              "175.184.164.215" "180.95.231.214"]]
     (ips->hosts ips 2 10)))