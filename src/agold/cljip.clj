(ns agold.cljip
  (:require [config.core :as e]
            [clojure.string :as str])
  (:gen-class))

(defn greet
  "Callable entry point to the application."
  [data]
  (println "conf key is: " (:API-KEY (e/load-env)))
  (println "foo is" (str/lower-case "Foo"))
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (greet {:name (first args)}))

(comment
  (greet {:name "Art"})
  (str/lower-case "FOO")
  #_(config.core/load-env)
  #_(e/load-env)
  )
