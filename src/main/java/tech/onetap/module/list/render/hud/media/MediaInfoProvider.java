package tech.onetap.module.list.render.hud.media;

import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Провайдер информации о текущем воспроизводимом медиа.
 * Реализации могут получать данные из Windows SMTC, Spotify, и т.д.
 */
public interface MediaInfoProvider {
    /**
     * Получить текущую информацию о воспроизводимом треке.
     * Вызывается из потока рендера, должен быть быстрым.
     */
    MediaInfo getCurrentMedia();

    /**
     * Проверка, работает ли провайдер на текущей платформе.
     */
    boolean isAvailable();

    /**
     * ID текстуры обложки (если провайдер сам управляет текстурой).
     */
    default Optional<Identifier> getCoverTextureId() {
        return Optional.empty();
    }
}
