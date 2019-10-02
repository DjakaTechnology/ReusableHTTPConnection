package id.djaka.netowrkhelpertest;

interface OnRequestFinished {
    void onSuccess(String jsonResponse);
    void onError(int errorCode);
}
