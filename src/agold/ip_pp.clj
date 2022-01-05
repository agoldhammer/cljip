(ns agold.ip-pp
  (:require [clojure.core.async :as a]
            [agold.dateparser :as dp]
            [io.aviso.ansi :as ansi]
            [clojure.pprint :as pp]))

(def exit-chan (a/chan 1))

#_(defn wait-counter
  "returns a fn f of two vars
   optype :incr or :decr
   val amount to increase or decrease the weight count
   on :decr, if count reaches zero, send msg :wait-done on wait-chan"
  [wait-chan]
  (let [counter (atom 0)]
    (fn [optype val]
      #_(println "optype, val, counter" optype val @counter)
      (cond
        (= optype :incr) (swap! counter + val)
        (= optype :decr) (do (swap! counter - val)
                             (when (zero? @counter)
                               #_(println "ctr is 0")
                               (a/offer! wait-chan :wait-done)))
        :else (throw IllegalArgumentException)))))


#_(defn start-print-loop
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
      (a/offer! exit-chan :exit-apc)
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
  #_(let [pch (start-print-loop)]
    (a/>!! pch smpl-le))
  (let [ch (a/to-chan! [1 2 {:a 3}])]
    (apply-to-channel pp/pprint ch))
  (le-reducer {} smpl-le)
  (get-in (le-reducer {} smpl-le) ["47.241.66.187" :events])
  (le-reducer (le-reducer {} smpl-le) smpl-le)
  (get-in (le-reducer (le-reducer {} smpl-le) smpl-le) ["47.241.66.187" :events]))

(defn site-data->strings
  "convert site data to a vector of strings"
  [data]
  (let [{:keys [country_code2 country_name city state_prov
                district latitude longitude]} data
        line1 (str "  " (ansi/bold-cyan country_name) " (" country_code2 ")")
        line2 (str "  " city ", " state_prov (when (not= "" district) (str " district: " district)))
        line3 (str "  lat: " latitude " lon: " longitude)]
    [line1 line2 line3]))

(defn pp-reduced-log-entry
  "pretty print reduced log entry rle"
  [ip data]
  (let [events (:events data)
        sd (:site-data data)
        sd-lines (site-data->strings sd)]
    (println (ansi/bold-yellow (str "ip: " ip)))
    (doseq [line sd-lines]
      (println line))
    (doseq [event events]
      (println "  **")
      (println "  date/time:" (ansi/bold-magenta (dp/jtime->datestr (:date event))))
      (println (str "  ...." (ansi/red (:entry event))))
      (println (str "  ...." (ansi/cyan (:req event)))))
  (println "---")))

(defn pp-reduced-log
  "pretty print reduced log"
  [rl]
  (doseq [[ip data] rl]
    (pp-reduced-log-entry ip data)))

(comment
  ;; sample site-data
  (def smpl-sd {:country_code2 "US"
                :city "Mountain View"
                :longitude "-122.08421"
                :zipcode "94043-1351"
                :country_name "United States"
                :country_code3 "USA"
                :latitude "37.42240"
                :state_prov "California"
                :district ""})


  (site-data->strings smpl-sd)
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

  (def smpl-rle2 {"35.89.25.35"
                  {:events
                   [{:entry
                     "35.89.25.35 - - [30/Dec/2021:03:06:04 +0000] \"GET /favicon.ico HTTP/1.1\""
                     :date
                     "dummy date1"
                     :req "GET /favicon.ico HTTP/1.1"}
                    {:entry
                     "35.89.25.35 - - [30/Dec/2021:03:06:05 +0000] \"GET / HTTP/1.1\""
                     :date
                     "dummy date2"
                     :req "GET / HTTP/1.1"}]
                   :site-data
                   {:country_code2 "US"
                    :city ""
                    :longitude "-123.04381"
                    :zipcode ""
                    :country_name "United States"
                    :country_code3 "USA"
                    :latitude "44.93326"
                    :state_prov "Oregon"
                    :district ""}}})
  ;; font coloring does not work in Calva output window
  (:site-data (get smpl-rle "35.233.62.116"))
  (pp/pprint smpl-rle)
  (println (str "test " (ansi/red "yellow")))
  (ansi/red "red")
  (println (str "should be bold yellow " ansi/bold-yellow-font "bold yellow"))
  (pp-reduced-log-entry "35.89.25.35" (get smpl-rle2 "35.89.25.35")))
