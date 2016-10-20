(ns trivia-cms.core
  (:require [trivia-cms.core :refer :all]
            [ring.adapter.jetty :as jetty]))

(defn -main []
  (jetty/run-jetty app {:port 3000}))
