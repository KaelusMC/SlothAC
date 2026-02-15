package space.kaelus.sloth.api

import java.util.Optional
import org.bukkit.Bukkit
import org.bukkit.plugin.RegisteredServiceProvider

/** Service loader for [SlothApi] via Bukkit ServicesManager. */
object SlothApiProvider {
  /**
   * Returns the Sloth API instance if SlothAC is present and registered.
   *
   * @return optional SlothApi
   */
  @JvmStatic
  fun get(): Optional<SlothApi> {
    val provider: RegisteredServiceProvider<SlothApi>? =
      Bukkit.getServicesManager().getRegistration(SlothApi::class.java)
    return if (provider == null) {
      Optional.empty()
    } else {
      Optional.ofNullable(provider.provider)
    }
  }
}
