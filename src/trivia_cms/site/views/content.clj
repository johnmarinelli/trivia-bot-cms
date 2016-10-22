(ns trivia-cms.site.views.content
  (:require [trivia-cms.api.api-backend :as api])
  (:use [hiccup.form]
        [hiccup.core]
        [hiccup.element :only (link-to)]))

(defn home []
  [:div "Home"])

(defn create-quiz []
  (form-to [:post "/quizzes/create"]
            (text-field {:class "form-control" :placeholder "My Quiz"} :quiz-name)
            (submit-button "Create New Quiz")))

(defn all-quizzes []
  (let [quizzes (api/get-quizzes)]
    (html [:ul
           (for [quiz quizzes]
             [:li 
              [:div.list-item 
               [:h1 (h (:quiz-name quiz))]]])])))

(defn quiz [quiz-name]
  (let [quiz (api/get-quiz quiz-name)
        questions (:questions quiz)]
    (html [:div.content
           [:h1 quiz-name]
           [:ul
            (for [question questions]

              (let [{:keys [question-body 
                            answer 
                            category 
                            value]} question] 
                [:div.list-item
                 [:h2 (h question-body)]
                 [:p (h category)]
                 [:p (h value)]
                 [:h3 (h answer)]]))]])))
