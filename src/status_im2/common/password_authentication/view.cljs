(ns status-im2.common.password-authentication.view
  (:require [react-native.core :as rn]
            [quo2.core :as quo]
            [utils.re-frame :as rf]
            [utils.i18n :as i18n]
            [reagent.core :as reagent]
            [status-im2.contexts.profile.utils :as profile.utils]
            [native-module.core :as native-module]))

(defn view
  []
  (let [entered-password (reagent/atom "")]
    (fn []
      (let [{:keys [primary-name] :as profile} (rf/sub [:profile/profile-with-image])
            {:keys [error button]}             (rf/sub [:password-authentication])]
        [rn/view {:padding-horizontal 20}
         [quo/text {:size :heading-1 :weight :semi-bold}
          (i18n/label :t/enter-password)]
         [rn/view {:style {:margin-top 8 :margin-bottom 20}}
          [quo/context-tag
           {:size            24
            :full-name       primary-name
            :profile-picture (profile.utils/photo profile)}]]
         [quo/input
          {:type           :password
           :label          (i18n/label :t/profile-password)
           :placeholder    (i18n/label :t/type-your-password)
           :error?         (when (not-empty error) error)
           :auto-focus     true
           :on-change-text #(reset! entered-password %)}]
         (when (not-empty error)
           [quo/info-message
            {:type  :error
             :size  :default
             :icon  :i/info
             :style {:margin-top 8}}
            (i18n/label :t/oops-wrong-password)])
         [quo/button
          {:container-style {:margin-bottom 12 :margin-top 40}
           :on-press        #((:on-press button) (native-module/sha3 @entered-password))}
          (:label button)]]))))
