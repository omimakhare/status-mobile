(ns status-im2.common.nickname-drawer.view
  (:require [clojure.string :as string]
            [quo.design-system.colors :as colors]
            [quo2.components.icon :as icons]
            [quo2.core :as quo]
            [react-native.core :as rn]
            [reagent.core :as reagent]
            [status-im2.common.nickname-drawer.style :as style]
            [utils.i18n :as i18n]
            [utils.re-frame :as rf]))

(defn bad-nickname?
  [nickname]
  (string/blank? @nickname))

(defn add-nickname-toast
  [primary-name entered-nickname public-key]
  (fn []
    (rf/dispatch [:hide-bottom-sheet])
    (rf/dispatch [:toasts/upsert
                  {:id         :add-nickname
                   :icon       :correct
                   :icon-color (:positive-01 @colors/theme)
                   :text       (i18n/label
                                :t/set-nickname-toast
                                {:primary-name primary-name
                                 :nickname     (string/trim @entered-nickname)})}])
    (rf/dispatch [:contacts/update-nickname public-key (string/trim @entered-nickname)])))

(defn nickname-drawer
  [{:keys [title description contact accessibility-label
           close-button-text]}]
  (let [{:keys [primary-name nickname public-key]} contact
        entered-nickname                           (reagent/atom nickname)
        photo-path                                 (when-not (empty? (:images contact))
                                                     (rf/sub [:chats/photo-path public-key]))]
    (fn []
      [rn/view
       {:style               style/nickname-container
        :accessibility-label accessibility-label}
       [quo/text
        {:weight :semi-bold
         :size   :heading-1} title]
       [rn/view {:style (style/context-container)}
        [quo/user-avatar
         {:full-name        primary-name
          :profile-picture  photo-path
          :size             :xxs
          :status-indicator false}]
        [quo/text
         {:weight :medium
          :size   :paragraph-2
          :style  {:margin-left 4}} primary-name]]
       [quo/input
        {:type              :text
         :blur?             true
         :placeholder       (i18n/label :t/type-nickname)
         :auto-focus        true
         :max-length        32
         :on-change-text    (fn [nickname]
                              (reset! entered-nickname nickname))
         :on-submit-editing (add-nickname-toast primary-name entered-nickname public-key)}]
       [rn/view
        {:style style/nickname-description-container}
        [icons/icon :i/info
         {:size 16}]
        [quo/text
         {:weight :regular
          :size   :paragraph-2
          :style  (style/nickname-description)}
         description]]
       [rn/view {:style style/buttons-container}
        [quo/button
         {:type     :grey
          :style    {:flex 0.48}
          :on-press #(rf/dispatch [:hide-bottom-sheet])}
         (or close-button-text (i18n/label :t/cancel))]

        [quo/button
         {:type     :primary
          :disabled (bad-nickname? entered-nickname)
          :style    {:flex 0.48}
          :on-press (add-nickname-toast primary-name entered-nickname public-key)}
         title]]])))
