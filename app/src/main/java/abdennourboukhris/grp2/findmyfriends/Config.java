package abdennourboukhris.grp2.findmyfriends;

public class Config {
    public static String IP_SERVEUR = "10.0.2.2:80";

    public static String BASE_URL = "http://" + IP_SERVEUR + "/servicephp/";
    public static String URL_GetAll_Location = BASE_URL + "get_all.php";
    public static String URL_POST_Location = BASE_URL + "add_position.php";

    public static String LOGIN_REGISTER = BASE_URL + "login_register.php";
    public static String CHECK_USER_EXISTS = BASE_URL + "check_user_exists.php";
    public static String SEND_OTP = BASE_URL + "send_otp.php";
    public static String VERIFY_OTP = BASE_URL + "verify_otp.php";

    public static String FRIENDSHIPS_CRUD = BASE_URL + "friendships_crud.php";

    public static String USERS_CRUD = BASE_URL + "users_crud.php";
}
