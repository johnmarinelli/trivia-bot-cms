(ns trivia-cms.handler
  (:require [trivia-cms.api.api-frontend :refer [api]]
            [trivia-cms.site.site-frontend :refer [site]]
            [compojure.core :refer [routes]]))

(def app
  (routes api site))
