(ns status-im2.contexts.wallet.scan-account.view
  (:require [utils.debounce :as debounce]
            [utils.i18n :as i18n]
            [status-im2.common.scan-qr-code.view :as scan-qr-code]
            [status-im2.constants :as constants]
            [clojure.string :as string]))

(defn- is-address
  [s]
  (boolean (re-matches constants/regx-address s)))

(defn- has-address?
  [scanned-text]
  (some #(is-address %) (string/split scanned-text #":")))

(defn- extract-address
  [scanned-text]
  (first (filter #(is-address %) (string/split scanned-text #":"))))

(defn view
  []
  [scan-qr-code/view
   {:title           (i18n/label :t/scan-qr)
    :subtitle        (i18n/label :t/scan-an-account-qr-code)
    :error-message   (i18n/label :t/oops-this-qr-does-not-contain-an-address)
    :validate-fn     #(has-address? %)
    :on-success-scan #(debounce/debounce-and-dispatch [:wallet-2/scan-address-success
                                                       (extract-address %)]
                                                      300)}])
