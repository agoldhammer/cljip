(ns agold.cljip-test
  (:require [clojure.test :refer [deftest testing is]]
            [agold.cljip :as a]))


(deftest config-test
  (testing "config loading"
    (is (not (nil? (:API-KEY (a/get-config)))))))
