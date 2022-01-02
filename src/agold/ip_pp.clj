(ns agold.ip-pp
  (:require [clojure.core.async :as a]
            [clojure.pprint :as pp]))

(def exit-chan (a/chan))

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
    (if (nil? item)
      (a/>! exit-chan :exit-apc)
      (do
        (f item)
        (recur (a/<! ch))))))

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

(defn pp-reduced-log-entry
  "pretty print reduced log entry rle"
  [rle]
  (let [ip (first (keys rle))
        data (get rle ip)
        events (:events data)
        sd (:site-data data)]
    (pp/pprint (str "ip: " ip))
    (pp/pprint sd)
    (doseq [event events] (pp/pprint event))))

(comment
  ;; this is a reduced log entry
  (def smpl-rle
    {"35.233.62.116"
     {:events
      [{:entry
        "35.233.62.116 - - [30/Dec/2021:12:18:33 +0000] \"GET / HTTP/1.1\""
        :date
        "dummy date"
        :req "GET / HTTP/1.1"}]
      :site-data
      {:country_code2 "US"
       :city "Mountain View"
       :longitude "-122.08421"
       :zipcode "94043-1351"
       :country_name "United States"
       :country_code3 "USA"
       :latitude "37.42240"
       :state_prov "California"
       :district ""}}})
  (:site-data (get smpl-rle "35.233.62.116"))
  (pp/pprint smpl-rle)
  (pp-reduced-log-entry smpl-rle))
