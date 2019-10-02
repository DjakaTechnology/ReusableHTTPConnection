package id.djaka.netowrkhelpertest;


import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class JavaListFragment extends Fragment {
    private View v;
    private FloatingActionButton fabAdd;
    private ListView listView;
    private ArrayList<HashMap<String, String>> userList = new ArrayList<>();
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_list, container, false);
        fabAdd = v.findViewById(R.id.fab_add_data);
        listView = v.findViewById(R.id.list_view);

        setButton();
        prepareData();
        return v;
    }

    private void setButton() {
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
    }

    private void showDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_user);

        Button submit = dialog.findViewById(R.id.btn_submit);
        final TextInputLayout nameInput = dialog.findViewById(R.id.input_name);
        final TextInputLayout jobInput = dialog.findViewById(R.id.input_job);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitUser(nameInput.getEditText().getText().toString(), jobInput.getEditText().getText().toString());
            }
        });

        dialog.show();
    }

    private void submitUser(String name, String job) {
        try {
            URL url = new URL("https://reqres.in/api/users");
            JSONObject obj = new JSONObject();
            obj.put(Keys.NAME_KEY, name);
            obj.put(Keys.JOB_KEY, job);
            networkHelper(url, "POST", new OnRequestFinished() {
                @Override
                public void onSuccess(String jsonResponse) {
                    onPostUserResponse(jsonResponse);
                }
            }, obj);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void prepareData() {
        try {
            URL url = new URL("https://reqres.in/api/users");
            networkHelper(url, "GET", new OnRequestFinished() {
                @Override
                public void onSuccess(String jsonResponse) {
                    onGetUserListResponse(jsonResponse);
                }
            }, null);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void onGetUserListResponse(String jsonString) {
        try {
            repopulateListData(new JSONObject(jsonString));
            renderListData();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onPostUserResponse(String jsonString) {
        try {
            JSONObject obj = new JSONObject(jsonString);

            Toast.makeText(getContext(), "User created with Id : "+ obj.getString("id") +", \nName: "+ obj.getString("name"), Toast.LENGTH_SHORT).show();
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void repopulateListData(JSONObject json) {
        userList.clear();

        try {
            JSONArray data = json.getJSONArray("data");

            for(int i = 0;i < data.length();i++) {
                JSONObject obj = data.getJSONObject(i);
                String name = obj.getString(Keys.FIRST_NAME_KEY);
                String email = obj.getString(Keys.EMAIL_KEY);

                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put(Keys.FIRST_NAME_KEY, name);
                hashMap.put(Keys.EMAIL_KEY, email);

                userList.add(hashMap);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void renderListData() {
        SimpleAdapter adapter = new SimpleAdapter(
                getContext(),
                userList,
                android.R.layout.simple_list_item_2,
                new String[]{Keys.FIRST_NAME_KEY, Keys.EMAIL_KEY},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        listView.setAdapter(adapter);
    }

    private void showError(int code) {
        Toast.makeText(getContext(), "Http error: " + code, Toast.LENGTH_SHORT).show();
    }

    private String streamToString(InputStream stream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
        String result = "";
        String line = "";

        while((line = bufferedReader.readLine()) != null) {
            result += line;
        }

        return result;
    }

    private void networkHelper(URL url, final String method, final OnRequestFinished onRequestFinished, final JSONObject param) {
        try{
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(method);
            con.setRequestProperty("Content-Type", "application/json");

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    if(method.equals("POST") && param != null) {
                        con.setDoOutput(true);
                        try {
                            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
                            out.write(param.toString());
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    int responseCode = 0;
                    try {
                        responseCode = con.getResponseCode();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(responseCode >= 200 && responseCode < 300) { //Check if responsecode is 2xx, coz 2xx means success
                        try {
                            final String result = streamToString(con.getInputStream());
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onRequestFinished.onSuccess(result);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        final int finalResponseCode = responseCode;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showError(finalResponseCode);
                            }
                        });
                    }
                }
            });
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}
