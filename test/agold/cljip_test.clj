(ns agold.cljip-test
  (:require [clojure.test :refer [deftest testing is]]
            [agold.cljip :as a]
            #_[agold.ipgeo :as ipg]))




(deftest parse-log-test
  (testing "log parsing"
    (is (=
         (:ip (first (a/parse-log "/home/agold/Prog/cljip/testdata/default.log")))
         "39.104.69.228"))))
