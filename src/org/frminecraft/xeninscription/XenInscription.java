package org.frminecraft.xeninscription;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

/**
 * Plugin to allow in-game registration for the XenForo forum software.
 *
 * @author MrUrfaneck
 */
public class XenInscription extends JavaPlugin {

    private static String site, apiHash, usernameField, uuidField;
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public void onEnable() {
        saveDefaultConfig();
        site = getConfig().getString("site");
        apiHash = getConfig().getString("apihash");
        usernameField = getConfig().getString("custom_field_names.minecraft_username");
        uuidField = getConfig().getString("custom_field_names.minecraft_uuid");

        if (!site.substring(site.length() - 1).equals("/")) {
            site = (site + "/");
        }
    }

    public void onDisable() {

    }

    public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
        if (cmd.getName().equalsIgnoreCase("register")) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Please specify your email address!");
                return false;
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
                    public void run() {
                        Random ran = new Random();
                        long x = (long) ran.nextInt(Integer.MAX_VALUE) + 999999L; // Generate random number password
                        String registrationStatus = registerUser(sender.getName(), x + "", args[0]);
                        sender.sendMessage(registrationStatus);
                    }
                });
            }
            return true;
        }
        return false;
    }

    /**
     * Registers a user on the forums
     *
     * @param user     Username of the user
     * @param password Password for the user. A randomly generated one is recommended.
     * @param email    Email address for the user
     * @return Status of the registration attempt
     */
    private static String registerUser(String user, String password, String email) {
        if (email.contains("=") || email.contains("&")) {
            return ChatColor.RED + "Potential injection attack! This event has been recorded!";
        }

        if (!email.matches(EMAIL_PATTERN)) {
            return ChatColor.RED + "Adresse mail invalide!";
        }

        String uuid = "null";
        Player p = Bukkit.getPlayer(user);
        if (p != null) uuid = p.getUniqueId().toString();

        try {
            String link = "api.php?action=register&hash=" + apiHash + "&username=" + user.replace('&', '.') +
                    "&password=" + password + "&email=" + email.replace('&', '.').replace('=', '.') +
                    "&custom_fields=" + usernameField + "=" + user + "," + uuidField + "=" + uuid + "&user_state=email_confirm";

            URL url = new URL(site + link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Open URL connection
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // User already exists?
                if (inputLine.contains("{\"error\":7,\"message\":\"Something went wrong when \\\"registering user\\\": \\\"" +
                        "User already exists\\\"\",\"user_error_id\":40,\"user_error_field\":\"username\",\"" +
                        "user_error_key\":\"usernames_must_be_unique\",\"user_error_phrase\":\"Usernames must be unique." +
                        " The specified username is already in use.\"}"))
                    return ChatColor.RED + "Un utilisateur avec ce nom existe déjà!";

                // Email already in use?
                if (inputLine.contains("{\"error\":7,\"message\":\"Something went wrong when \\\"registering user\\\": \\\"" +
                        "Email already used\\\"\",\"user_error_id\":42,\"user_error_field\":\"email\",\"user_error_key\":\"" +
                        "email_addresses_must_be_unique\",\"user_error_phrase\":\"Email addresses must be unique. " +
                        "The specified email address is already in use.\"}"))
                    return ChatColor.RED + "Un utilisateur avec cet email existe déjà!";
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ChatColor.RED + "Erreur! The stack trace has been printed to the server's console!";
        } catch (IOException e) {
            e.printStackTrace();
            return ChatColor.RED + "Erreur! The stack trace has been printed to the server's console!";
        }

        return ChatColor.DARK_AQUA + "Bravo ! Vous êtes bien enregistré sur le forum! Connectez-vous sur https://fr-minecraft.org/login \n" +
                "Un email de confirmation a été envoyé sur l'adresse mail que vous avez indiqué. Cliquez sur le lien du mail pour confirmer votre inscription. " +
                "Un mot de passe a été généré aléatoirement et vous sera envoyé par mail. Vous pouvez ensuite le changer sur votre compte forum!";
    }

}

