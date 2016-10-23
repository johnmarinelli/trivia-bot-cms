(ns trivia-cms.errors.api-error)

(defn api-error [msg]
  {:error-message msg})
