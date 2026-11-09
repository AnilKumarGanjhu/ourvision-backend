package com.ourvision.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.ourvision.entity.ProgramMedia;

public interface ProgramMediaRepository extends JpaRepository<ProgramMedia, Long> {

    @Query("select m from ProgramMedia m where m.mediaType in ('PHOTO', 'VIDEO') order by m.createdAt desc")
    List<ProgramMedia> findGalleryMedia();
}
