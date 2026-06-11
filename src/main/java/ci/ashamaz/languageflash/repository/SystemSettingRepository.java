package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
