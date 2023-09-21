(ns status-im2.contexts.profile.utils)

(defn photo
  [{:keys [images]}]
  (or (:large images)
      (:thumbnail images)
      (first images)))
