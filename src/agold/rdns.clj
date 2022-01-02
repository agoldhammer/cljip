#_:clj-kondo/ignore
(ns agold.rdns
  (:require [clojure.core.async :refer [put! close!]])
  (:import [java.util.concurrent Executors]
           [java.net InetAddress]))


#_:clj-kondo/ignore
(defn reverse-dns-lookup
  "homespun version of reverse dns"
  [ip]
  (.getCanonicalHostName (InetAddress/getByName ip)))

#_(defn async-wrapper
  "execute blocking fn f in a thread pool and put results on channel outch"
  [pool outch f & args]
  (.submit pool (fn []
                  (try (put! outch (apply f args))
                       (catch Exception e (put! outch (ex-info "wrapper err" {:args args})))
                       (finally (close! outch))))))

#_(defn rdns-fetch
  "do reverse dns lookup on multiple threads"
  [ips]
  (let [nitems (count ips)]))