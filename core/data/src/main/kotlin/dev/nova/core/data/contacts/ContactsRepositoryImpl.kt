package dev.nova.core.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nova.core.common.AppDispatchers
import dev.nova.core.domain.model.SearchResult
import dev.nova.core.domain.repository.ContactsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Busca contatos por nome. Sem READ_CONTACTS concedida devolve vazio (nunca pede a permissão sozinho). */
@Singleton
class ContactsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) : ContactsRepository {

    override suspend fun search(query: String, limit: Int): List<SearchResult.Contact> = withContext(dispatchers.io) {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }
        val out = ArrayList<SearchResult.Contact>()
        try {
            val name = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, name),
                "$name LIKE ?",
                arrayOf("%$query%"),
                "$name ASC LIMIT $limit",
            )?.use { c ->
                while (c.moveToNext()) out += SearchResult.Contact(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty())
            }
        } catch (_: SecurityException) {
            // permissão revogada durante a consulta
        }
        out
    }
}
