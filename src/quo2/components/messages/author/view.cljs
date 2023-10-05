(ns quo2.components.messages.author.view
  (:require [clojure.string :as string]
            [quo2.components.icon :as icons]
            [quo2.components.markdown.text :as text]
            [quo2.components.messages.author.style :as style]
            [react-native.core :as rn]
            [quo2.theme :as quo.theme]))

(def middle-dot "·")

(defn- internal-view
  [{:keys [primary-name secondary-name style short-chat-key time-str contact? verified? untrustworthy?
           muted? size theme]
    :or   {size 13}}]
  [rn/view {:style (merge style/container style {:height (if (= size 15) 21.75 18.2)})}
   [text/text
    {:weight              :semi-bold
     :size                (if (= size 15) :paragraph-1 :paragraph-2)
     :number-of-lines     1
     :accessibility-label :author-primary-name
     :style               (style/primary-name muted? theme)}
    primary-name]
   (when (not (string/blank? secondary-name))
     [:<>
      [text/text
       {:size            :label
        :number-of-lines 1
        :style           (style/middle-dot-nickname theme)}
       middle-dot]
      [text/text
       {:weight              :medium
        :size                :label
        :number-of-lines     1
        :accessibility-label :author-secondary-name
        :style               (style/secondary-name theme)}
       secondary-name]])
   (when contact?
     [icons/icon :main-icons2/contact
      {:size            12
       :no-color        true
       :container-style style/icon-container}])
   (cond
     verified?
     [icons/icon :main-icons2/verified
      {:size            12
       :no-color        true
       :container-style style/icon-container}]
     untrustworthy?
     [icons/icon :main-icons2/untrustworthy
      {:size            12
       :no-color        true
       :container-style style/icon-container}])
   (when (and (not verified?) short-chat-key)
     [text/text
      {:monospace       true
       :size            :label
       :number-of-lines 1
       :style           (style/chat-key-text theme)}
      short-chat-key])
   (when (and (not verified?) time-str short-chat-key)
     [text/text
      {:monospace       true
       :size            :label
       :number-of-lines 1
       :style           (style/middle-dot-chat-key theme)}
      middle-dot])
   (when time-str
     [text/text
      {:monospace           true
       :size                :label
       :accessibility-label :message-timestamp
       :number-of-lines     1
       :style               (style/time-text theme)}
      time-str])])

(def view (quo.theme/with-theme internal-view))
