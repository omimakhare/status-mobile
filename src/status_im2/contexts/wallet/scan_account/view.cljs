(ns status-im2.contexts.wallet.scan-account.view
  (:require [utils.address :as address-utils]
            [utils.debounce :as debounce]
            [utils.i18n :as i18n]
            [status-im2.common.scan-qr-code.view :as scan-qr-code]))

(defn view
  []
  [scan-qr-code/view
   {:title           (i18n/label :t/scan-qr)
    :subtitle        (i18n/label :t/scan-an-account-qr-code)
    :error-message   (i18n/label :t/oops-this-qr-does-not-contain-an-address)
    :validate-fn     #(address-utils/address? %)
    :on-success-scan #(debounce/debounce-and-dispatch [:wallet-2/scan-address-success %] 300)}])
