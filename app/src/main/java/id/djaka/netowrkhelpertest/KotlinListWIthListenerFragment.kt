package id.djaka.netowrkhelpertest

import android.app.Dialog
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.dialog_user.*
import kotlinx.android.synthetic.main.fragment_list.view.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


class KotlinListWIthListenerFragment : Fragment() {
    private val userList = arrayListOf(hashMapOf<String, String>())
    lateinit var v: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v = inflater.inflate(R.layout.fragment_list, container, false)

        setButton()
        prepareData()

        return v
    }

    fun setButton() {
        v.fab_add_data.setOnClickListener {
            showDialog()
        }
    }

    fun showDialog() {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_user)

        dialog.btn_submit.setOnClickListener {
            submitUser(dialog.input_name.editText?.text.toString(), dialog.input_job.editText?.text.toString())
        }

        dialog.show()
    }

    private fun submitUser(name: String, job: String) {
        val url = URL("https://reqres.in/api/users")
        val obj = JSONObject()
        obj.put(Keys.NAME_KEY, name)
        obj.put(Keys.JOB_KEY, job)
        networkHelper(url, "POST", OnRequestFinished {
            onPostUserResponse(it)
        }, obj)
    }

    private fun prepareData() {
        val url = URL("https://reqres.in/api/users")

        networkHelper(url, "GET", OnRequestFinished {
            onGetUserListResponse(it)
        })
    }

    private fun renderListData() {
        val adapter = SimpleAdapter(
            context,
            userList,
            android.R.layout.simple_list_item_2,
            arrayOf(Keys.FIRST_NAME_KEY, Keys.EMAIL_KEY),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )

        v.list_view.adapter = adapter
    }

    private fun repopulateListData(json: JSONObject) {
        userList.clear()

        val data = json.getJSONArray("data")

        for(i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val name = obj.getString(Keys.FIRST_NAME_KEY)
            val email = obj.getString(Keys.EMAIL_KEY)

            userList.add(hashMapOf(Keys.FIRST_NAME_KEY to name, Keys.EMAIL_KEY to email))
        }
    }

    private fun onGetUserListResponse(jsonString: String) {
        repopulateListData(JSONObject(jsonString))
        renderListData()
    }

    private fun onPostUserResponse(jsonString: String) {
        val obj = JSONObject(jsonString)

        Toast.makeText(context, "User created with Id : ${obj.getString("id")}, \nName: ${obj.getString("name")}", Toast.LENGTH_SHORT).show()
    }

    private fun showError(code: Int) {
        Toast.makeText(context, "Http error: $code", Toast.LENGTH_SHORT).show()
    }

    //Helper
    private fun streamToString(stream: InputStream): String {
        val bufferedReader = BufferedReader(InputStreamReader(stream))
        var result = ""

        for(line in bufferedReader.readLines())
            result += line

        stream.close()
        return result
    }

    // Dynamic function
    private fun networkHelper(
        url: URL, method: String, onComplete: OnRequestFinished, param: JSONObject = JSONObject()) {
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = method
        con.setRequestProperty("Content-Type", "application/json")

        try {
            AsyncTask.execute {
                if (method != "GET") {
                    con.doOutput = true

                    val out = OutputStreamWriter(con.outputStream)
                    out.write(param.toString())
                    out.close()
                }

                val responseCode = con.responseCode
                if(responseCode in 200..299) { //Check if responsecode is 2xx, coz 2xx means success
                    val result = streamToString(con.inputStream)

                    activity?.runOnUiThread {
                        onComplete.onSuccess(result)
                    }
                } else {
                    activity?.runOnUiThread{
                        showError(responseCode)
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            con.disconnect()
        }
    }
}
