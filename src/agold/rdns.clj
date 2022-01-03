#_:clj-kondo/ignore
(ns agold.rdns
  (:require [clojure.core.async :refer [put! close! thread] :as a])
  (:import [java.util.concurrent Executors]
           [java.net InetAddress]))

(defn make-thread-pool
  "make pool of nthreads"
  [nthreads]
  (Executors/newFixedThreadPool nthreads))


#_:clj-kondo/ignore
(defn reverse-dns-lookup
  "homespun version of reverse dns"
  [ip]
  (.getCanonicalHostName (InetAddress/getByName ip)))

#_(a/<!! (thread (reverse-dns-lookup "207.244.248.240")))

(defn async-wrapper
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

(comment 
  (def pool (make-thread-pool 5))
  (def dnsch (a/chan 10))
  (async-wrapper pool dnsch reverse-dns-lookup "35.233.62.116")
  (a/go (println (a/<! dnsch)))
  )