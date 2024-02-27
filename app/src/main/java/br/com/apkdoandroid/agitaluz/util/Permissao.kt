package br.com.apkdoandroid.agitaluz.util

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class Permissao {

    companion object{
        private  var temPermissao = false;
        fun requisitarPermissoes(activity: Activity, permissoes: List<String>, requestCode: Int) : Boolean{

            //Verificar permissões negadas, para então solicitar
            val permissoesNegadas = mutableListOf<String>()
            permissoes.forEach { permissao ->

                 temPermissao = ContextCompat.checkSelfPermission(
                    activity, permissao
                ) == PackageManager.PERMISSION_GRANTED

                if( !temPermissao ){
                    permissoesNegadas.add(permissao)
                }
                Log.i("novas_permissoes", "permissoes: ${permissoesNegadas.size}")
                Log.i("novas_permissoes", "permissoes: ${temPermissao}")
            }

            //Requisitar permissões negadas pelo usuário
            if( permissoesNegadas.isNotEmpty() ){
                ActivityCompat.requestPermissions(
                    activity, permissoesNegadas.toTypedArray() , requestCode
                )
            }

            return temPermissao
        }
    }
}