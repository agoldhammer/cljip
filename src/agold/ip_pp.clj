(ns agold.ip-pp
  (:require [clojure.core.async :as a]
            [clojure.pprint :as pp]))

#_(defn pp-le-from-chan
    "pretty print log entry from channel"
    [pch]
    (a/go (pp/pprint (a/<! pch))))

(defn start-print-loop
  "create print channel for pretty printing log entries"
  []
  (let [prn-chan (a/chan 2048)]
    (a/go-loop [item (a/<! prn-chan)]
      (when item
        (pp/pprint item)
        (recur (a/<! prn-chan))))
    prn-chan))

(defn apply-to-channel
  "apply f (fn of 1 var) to each item in channel ch"
  [f ch]
  (a/go-loop [item (a/<! ch)]
    (when item
      (f item)
      (recur (a/<! ch))))
  :apply-to-channel-closing)

#_(defn le-reducer
    "reduce seq of log entries for output:
   reduced will be map {ip-as-string [vec of les w/o ip ...] ...}"
    [acc log-entry]
    (let [ip (:ip log-entry)
          stripped-le (dissoc log-entry :ip)]
      #_(println ip stripped-le)
      (if-let [les (get acc ip)]
        (assoc acc ip (conj les stripped-le))
        (assoc acc ip [stripped-le]))))

(defn le-reducer
  "reduce seq of log entries for output:
   reduced will be map {ip-as-string {:event [vec of les w/o ip ...]} ...}"
  [acc log-entry]
  (let [ip (:ip log-entry)
        stripped-le (dissoc log-entry :ip)]
    #_(println ip stripped-le)
    (if-let [les (get-in acc [ip :events])]
      (assoc acc ip {:events (conj les stripped-le)})
      (assoc acc ip {:events [stripped-le]}))))

(comment
  (def smpl-le {:entry
                "47.241.66.187 - - [27/Feb/2021:13:30:39 +0000] \"GET / HTTP/1.1\""
                :ip "47.241.66.187"
                :date
                "date object here"
                :req "GET / HTTP/1.1"})
  (let [pch (start-print-loop)]
    (a/>!! pch smpl-le))
  (let [ch (a/to-chan! [1 2 {:a 3}])]
    (apply-to-channel pp/pprint ch))
  (le-reducer {} smpl-le)
  (get-in (le-reducer {} smpl-le) ["47.241.66.187" :events])
  (le-reducer (le-reducer {} smpl-le) smpl-le)
  (get-in (le-reducer (le-reducer {} smpl-le) smpl-le) ["47.241.66.187" :events]))
