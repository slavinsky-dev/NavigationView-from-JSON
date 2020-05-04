package com.slavinskydev.jsontestgcpsoftware;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static DrawerLayout drawerLayout;
    private static NavigationView navigationView;
    private final String BASE_JSON_URL = "https://www.dropbox.com/s/fk3d5kg6cptkpr6/menu.json?dl=1";
    private static ArrayList<String> menuItemName = new ArrayList<>();
    private static ArrayList<String> menuFunction = new ArrayList<>();
    private static ArrayList<String> menuParameter = new ArrayList<>();
    boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        drawerLayout.closeDrawers();
                        FragmentFactory fragmentFactory = new FragmentFactory();
                        Fragment fragment = fragmentFactory.getFragmentById(menuItem.getItemId());
                        Bundle bundle = new Bundle();
                        bundle.putString("parameter", menuParameter.get(menuItem.getItemId()));
                        fragment.setArguments(bundle);
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.frameLayout, fragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        return true;
                    }
                });
        navigationView.getMenu().clear();
        menuItemName.clear();
        menuFunction.clear();
        menuParameter.clear();
        DownloadJSONTask downloadJSONTask = new DownloadJSONTask(getSupportFragmentManager());
        downloadJSONTask.execute(BASE_JSON_URL);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            return;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    public static class FragmentFactory {
        private Map<String, Class<? extends Fragment>> menuItemFragments;
        FragmentFactory() {
            menuItemFragments = new HashMap<>();
            menuItemFragments.put("text", TextFragment.class);
            menuItemFragments.put("image", ImageFragment.class);
            menuItemFragments.put("url", URLFragment.class);
        }
        Fragment getFragmentById(int id) {
            Class<? extends Fragment> fragmentClass = menuItemFragments.get(menuFunction.get(id));
            if (fragmentClass == null) throw new NullPointerException("fragment not found");
            try {
                return fragmentClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("failed to construct fragment", e);
            }
        }
    }

    private static class DownloadJSONTask extends AsyncTask<String, Void, String> {
        FragmentManager fragmentManager;
        DownloadJSONTask(FragmentManager fragmentManager) {
            this.fragmentManager = fragmentManager;
        }

        @Override
        protected String doInBackground(String... strings) {
            URL url;
            HttpsURLConnection httpsURLConnection = null;
            StringBuilder stringBuilder = new StringBuilder();
            try {
                url = new URL(strings[0]);
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
                InputStream inputStream = httpsURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line);
                    line = bufferedReader.readLine();
                }
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (httpsURLConnection != null) {
                    httpsURLConnection.disconnect();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                JSONArray menuJSON = jsonObject.getJSONArray("menu");
                for (int i = 0; i < menuJSON.length(); i++) {
                    String name = jsonObject.getJSONArray("menu").getJSONObject(i).getString("name");
                    String function = jsonObject.getJSONArray("menu").getJSONObject(i).getString("function");
                    String parameter = jsonObject.getJSONArray("menu").getJSONObject(i).getString("param");
                    menuItemName.add(name);
                    menuFunction.add(function);
                    menuParameter.add(parameter);
                }
                Menu menu = navigationView.getMenu();
                for (int i = 0; i < menuItemName.size(); i++) {
                    menu.add(R.id.groupMenu, i, i, menuItemName.get(i));
                }
                navigationView.getMenu().getItem(0).setChecked(true);
                FragmentFactory fragmentFactory = new FragmentFactory();
                Fragment fragment = fragmentFactory.getFragmentById(0);
                Bundle bundle = new Bundle();
                bundle.putString("parameter", menuParameter.get(0));
                fragment.setArguments(bundle);
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.frameLayout, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
