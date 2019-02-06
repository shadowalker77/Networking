package ir.ayantech.ayannetworking.api;

import ir.ayantech.ayannetworking.ayanModel.AyanRequest;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ApiInterface {
    @POST
    Call<ResponseBody> callApi(@Url String url, @Body AyanRequest request);
}
