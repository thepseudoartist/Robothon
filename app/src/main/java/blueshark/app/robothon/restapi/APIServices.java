package blueshark.app.robothon.restapi;

import blueshark.app.robothon.models.UserIDetails;
import blueshark.app.robothon.models.UserStatusResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface APIServices {

    @POST("/monitor/checkUserStatus")
    Call<UserStatusResponse> getUserStatus(@Body UserIDetails details);


}
