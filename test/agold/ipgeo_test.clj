(ns agold.ipgeo-test
  (:require [clojure.test :refer [deftest testing is]]
            #_[agold.cljip :as a]
            [agold.ipgeo :as ipg]))

(deftest config-test
  (testing "config loading"
    (is (not (nil? (:API-KEY (ipg/get-config)))))))

(deftest dns-test
  (testing "reverse dns if host exists"
    (is (= (ipg/get-hostname "100.35.79.95")
           {:hostname "pool-100-35-79-95.nwrknj.fios.verizon.net"})))
  (testing "reverse dns if host does not exist"
    (is (= (ipg/get-hostname "190.35.79.95")
           {:hostname "190.35.79.95"}))))