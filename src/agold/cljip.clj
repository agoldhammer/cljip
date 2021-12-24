(ns agold.cljip
  (:require [config.core :as e])
  (:gen-class))

(defn greet
  "Callable entry point to the application."
  [data]
  (println "conf key is: " (:API-KEY (e/load-env)))
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

(comment
  (e/load-env))
