(ns agold.dateparser
  #_{:clj-kondo/ignore [:unused-import]}
  (:import (java.time ZonedDateTime format.DateTimeFormatter)))

#_{:clj-kondo/ignore [:unresolved-namespace]}
(def formatter (DateTimeFormatter/ofPattern "yyyyMMdd:HHmmss z"))

(def month-map
  {"Jan" "01" "Feb" "02" "Mar" "03" "Apr" "04" "May" "05" "Jun" "06"
   "Jul" "07" "Aug" "08" "Sep" "09" "Oct" "10" "Nov" "11" "Dec" "12"})

(get month-map "Jan")

(defn- datestr->yyyyMMdd
  "converts date string 27/Feb/2021:00:58:22 from log to yyyyMMdd:hhMMss string"
  [datestr]
  (let [;;re #"(\S+)/(\S+)/(\S+?):"
        re #"(\S+)/(\S+)/(\S+?):(\S+):(\S+):(\S+)"
        parsed (re-find re datestr)]
    (str (parsed 3) (get month-map (parsed 2)) (parsed 1) ":" (parsed 4) (parsed 5)
         (parsed 6) " GMT+00:00")))

(defn datestr->jtime
  "converts log date str 27/Feb/2021:00:58:22 to Java date object"
  [datestr]
  (ZonedDateTime/parse (datestr->yyyyMMdd datestr) formatter))

(comment
  (def datestr "27/Feb/2021:00:58:22")
  (re-find #"(\S+)/(\S+)/(\S+?):(\S+):(\S+):(\S+)" datestr)
  (datestr->yyyyMMdd "27/Feb/2021:00:58:22")
  (ZonedDateTime/parse "20210727:005822 GMT+00:00" formatter)
  (datestr->jtime "27/Feb/2021:00:58:22")
  )