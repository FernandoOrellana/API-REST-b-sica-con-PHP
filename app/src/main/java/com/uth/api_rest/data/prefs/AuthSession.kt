package com.uth.api_rest.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.uth.api_rest.data.model.Usuario

object AuthSession {

    private const val PREFS = "auth_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_ID = "auth_user_id"
    private const val KEY_USUARIO = "auth_usuario"
    private const val KEY_NOMBRE = "auth_nombre"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun guardarSesion(context: Context, token: String, usuario: Usuario) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, usuario.ID_Usuario)
            .putString(KEY_USUARIO, usuario.usuario)
            .putString(KEY_NOMBRE, usuario.nombre)
            .commit()
    }

    fun obtenerToken(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)

    fun obtenerUsuario(context: Context): Usuario? {
        val id = prefs(context).getInt(KEY_USER_ID, -1)
        return if (id == -1) null else Usuario(
            id,
            prefs(context).getString(KEY_USUARIO, "") ?: "",
            "",
            prefs(context).getString(KEY_NOMBRE, "") ?: ""
        )
    }

    fun haySesion(context: Context): Boolean =
        prefs(context).getString(KEY_TOKEN, null) != null

    fun limpiarSesion(context: Context) {
        prefs(context).edit().clear().commit()
    }
}
