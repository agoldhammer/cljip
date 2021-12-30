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

(comment
  (def smpl-le {:entry
                "47.241.66.187 - - [27/Feb/2021:13:30:39 +0000] \"GET / HTTP/1.1\""
                :ip "47.241.66.187"
                :date
                "date object here"
                :req "GET / HTTP/1.1"
                :site-data
                {:country_code2 "SG"
                 :city "Singapore"
                 :longitude "103.85098"
                 :zipcode "048616"
                 :country_name "Singapore"
                 :country_code3 "SGP"
                 :latitude "1.28434"
                 :state_prov ""
                 :district "Central Business District"}})
  (let [pch (start-print-loop)]
    (a/>!! pch smpl-le))
  )
