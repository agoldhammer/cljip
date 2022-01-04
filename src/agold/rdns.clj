#_:clj-kondo/ignore
(ns agold.rdns
  (:require [clojure.core.async :refer [put! close! thread] :as a])
  (:import [java.util.concurrent Executors]
           [java.net InetAddress]))

#_(defn make-thread-pool
  "make pool of nthreads"
  [nthreads]
  (Executors/newFixedThreadPool nthreads))


#_:clj-kondo/ignore
(defn reverse-dns-lookup
  "homespun version of reverse dns"
  [ip]
  {:hostname (.getCanonicalHostName (InetAddress/getByName ip))})

#_(a/<!! (thread (reverse-dns-lookup "207.244.248.240")))

#_(defn async-wrapper
  "execute blocking fn f in a thread pool and put results on channel outch"
  [pool outch f & args]
  (.submit pool (fn []
                  (try (put! outch (apply f args))
                       (catch Exception e (put! outch (ex-info "wrapper err" {:args args} e)))
                       (finally (close! outch))))))

#_(defn rdns-fetch
  "do reverse dns lookup on multiple threads"
  [ips]
  (let [nitems (count ips)]))
(time
 (let [outch (a/chan 2048)
       ips ["39.104.69.228" "71.192.181.208" "124.88.55.27" "180.95.238.249"
            "175.184.164.215" "180.95.231.214"]]
   (a/pipeline-blocking 16 outch (map reverse-dns-lookup) (a/to-chan!! ips))
   (a/<!! (a/into [] outch))))

(comment 
  #_(def pool (make-thread-pool 5))
  #_(def dnsch (a/chan 10))
  #_(async-wrapper pool dnsch reverse-dns-lookup "35.233.62.116")
  #_(a/go (println (a/<! dnsch)))
  )