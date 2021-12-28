(ns agold.dateparser-test
  (:require [clojure.test :refer [deftest testing is]]
            [agold.dateparser :as dp])
  (:import (java.time ZonedDateTime)))

(deftest jtime-conv-test
  (testing "conversion of log date format to jtime zoned date"
    (is (= (type (dp/datestr->jtime "27/Feb/2021:00:58:22"))
           ZonedDateTime))))