(ns agold.cljip-test
  (:require [clojure.test :refer [deftest testing is]]
            [agold.cljip :as a]))


(deftest config-test
  (testing "config loading"
    (is (not (nil? (:API-KEY (a/get-config)))))))

(deftest dns-test
  (testing "reverse dns if host exists"
  (is (= (a/get-hostname "100.35.79.95")
         {:hostname "pool-100-35-79-95.nwrknj.fios.verizon.net"})))
  (testing "reverse dns if host does not exist"
    (is (= (a/get-hostname "190.35.79.95")
           {:hostname "190.35.79.95"})))
  )
