(ns trivia-cms.api.api-frontend
  (:require [clojure.java.io]
            [clojure.data.json :as json]

            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]

            [ring.util.codec :as codec]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            
            [trivia-cms.api.api-backend :refer :all])

  (:use [ring.util.response :only [response not-found]] ; wrap json response
))

(defroutes api-routes

  (GET "/api/quizzes" [request] 
       (response 
        (map
         (fn [q] 
           (update-in q [:_id] #(.toString %)))
         (get-quizzes))))

  (POST "/api/quizzes/create" req
        (let [params (:params req)
              quiz-name (:quiz-name params)
              questions (or (:questions params) [])]
          (if (some nil? [params quiz-name])
            {:status 400 
             :body {:error-message  "Name is required when creating quizzes."} }
            (response 
             (save-quiz! {:quiz-name quiz-name :questions questions})))))

  (DELETE "/api/quizzes/:name" [name]
          (let [num-deleted (delete-quiz name)]
            (if (> num-deleted 0)
              (response {:num-deleted num-deleted})
              (not-found 
               (json/write-str {:error-message (str "Quiz '" name "' not found.")})))))

  (GET "/api/quizzes/:name" [name]
       (let [quiz (get-quiz name)]
         (if (nil? quiz)
           (not-found {:error-message (str "Quiz '" name "' not found.")})
           (response quiz))))

  (DELETE "/api/quizzes/:quiz-name/questions/:id" 
          {:keys [headers params body] :as request} 
          (let [{:keys [quiz-name id]} params]
            (let [num-questions (count (:questions (get-quiz quiz-name)))
                  num-deleted (- num-questions (count 
                                                (:questions 
                                                 (delete-question quiz-name id))))]
              (if (> num-deleted 0)
                (response num-deleted)
                (not-found
                 (json/write-str {:error-message "Question id '" id "' not found."}))))))

  ; Saves a question to the given quiz
  ; Returns the modified quiz
  (POST "/api/quizzes/:quiz-name/questions/create" [quiz-name & params]
        (let [{:keys [question-body
                      category
                      answer
                      value]} params]
          (if (some nil? [quiz-name question-body category answer value])
            {:status 400
             :body {:error-message "All fields are required when creating questions."}}
            (response 
             (update-in
              (save-question! quiz-name {:question-body question-body
                                         :category category
                                         :answer answer
                                         :value value})
              [:_id]
              #(.toString %)))))))

(def api
  (handler/api (->
                 api-routes
                 (wrap-defaults api-defaults)
                 (wrap-json-params)
                 (wrap-json-response))))


