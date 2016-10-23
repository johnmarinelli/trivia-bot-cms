(ns trivia-cms.api.api-frontend
  (:require [clojure.java.io]
            [clojure.data.json :as json]

            [compojure.core :refer :all]
            [compojure.route :as route]

            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]

            [trivia-cms.models.quiz :as quiz]
            [trivia-cms.models.question :as question])

  (:use [ring.util.response :only [response not-found]] ; wrap json response
))

(defroutes api-routes

  (GET "/api/quizzes" 
       [request] 
       (let [res (quiz/find-models {})]
         (map #(quiz/serialize %) res)))

  (GET ["/api/quizzes/:name" :name #".{1,16}"]
       [name]
       (let [quizzes (quiz/find-models {:quiz-name name})]
         (if (empty? quizzes)
           (not-found {:error-message (str "Quiz '" name "' not found.")})
           (response 
            (-> (first quizzes)
                (quiz/serialize))))))

  ; /.{n,}/ is regex for "anything with n characters".
  ; since question names are capped to 16 characters, we 
  ; assume anything longer than 16 characters is a quiz id.
  (GET ["/api/quizzes/:id" :id #".{17,}"]
       [id]
       (let [quiz (first (quiz/find-models {:_id id}))]
         (if (nil? quiz)
           (not-found {:error-message (str "Quiz with id '" id "' not found.")})
           (response
            (-> quiz
                (quiz/serialize))))))

  (POST "/api/quizzes/create" req
        (let [params (:params req)
              quiz-name (:quiz-name params)
              questions (or (:questions params) [])
              validated (not (some nil? [params quiz-name]))]
          (if (not validated)
            {:status 400 
             :body {:error-message  "Name is required when creating quizzes."} }
            (response 
             (-> (quiz/create params)
                 (quiz/serialize))))))

  (POST ["/api/quizzes/:quiz-name/questions" :quiz-name #".{1,16}"]
        [quiz-name & params]
        (let [quiz (first (quiz/find-models {:quiz-name quiz-name}))
              {:keys [body answer category value]} params
              validated (not (some nil? [quiz body answer category value]))]
          (if (not validated)
            {:status 400
             :body {:error-message (str "Question could not be created.  Were all values filled out?  Is it a valid quiz?")}}
            (let [question (question/create params)]
              (let [new-quiz (quiz/add-questions quiz [question])]
                (response
                 (quiz/serialize new-quiz)))))))

  (DELETE "/api/quizzes/:id" [id]
          (let [num-deleted (quiz/destroy id)]
            (if (> num-deleted 0)
              (response {:num-deleted num-deleted})
              (not-found 
               (json/write-str {:error-message (str "Quiz '" name "' not found.")})))))

  (DELETE "/api/quizzes/:quiz-id/questions/:question-id" 
          [quiz-id question-id]
          (let [quiz (first (quiz/find-models {:_id quiz-id}))
                res (quiz/remove-questions quiz [question-id])]
            (response 
             (quiz/serialize res)))))

(def api
  (->
   api-routes
   (wrap-defaults api-defaults)
   (wrap-json-params)
   (wrap-json-response)))


