/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.repository.support;

import jakarta.persistence.EntityManager;

import java.util.List;

import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Specialty;
import org.springframework.samples.petclinic.model.Visit;

/**
 * Cascading delete operations shared by the {@code jpa} and {@code spring-data-jpa} repository
 * implementations.
 * <p>
 * Both profiles work against the same {@link EntityManager} and therefore need the exact same
 * cascade semantics. Keeping the queries here means a change to how an entity cascades is made
 * once instead of being kept in sync across two packages.
 * <p>
 * The {@code jdbc} profile deliberately does <em>not</em> use this class: it cascades with plain
 * SQL, because only the H2 schema declares {@code ON DELETE CASCADE} — hsqldb, mysql and postgres
 * do not, so the database cannot be relied upon to cascade for us.
 */
public final class JpaCascadeDeleteSupport {

    private JpaCascadeDeleteSupport() {
    }

    /**
     * Deletes a pet together with its visits.
     */
    public static void deletePet(EntityManager em, Pet pet) {
        Integer petId = pet.getId();
        em.createQuery("DELETE FROM Visit visit WHERE pet.id = :petId")
            .setParameter("petId", petId)
            .executeUpdate();
        em.createQuery("DELETE FROM Pet pet WHERE id = :petId")
            .setParameter("petId", petId)
            .executeUpdate();
        if (em.contains(pet)) {
            em.remove(pet);
        }
    }

    /**
     * Deletes a pet type together with every pet of that type and those pets' visits.
     */
    public static void deletePetType(EntityManager em, PetType petType) {
        em.remove(em.contains(petType) ? petType : em.merge(petType));
        Integer petTypeId = petType.getId();

        List<Pet> pets = em.createQuery("SELECT pet FROM Pet pet WHERE type.id = :petTypeId", Pet.class)
            .setParameter("petTypeId", petTypeId)
            .getResultList();
        for (Pet pet : pets) {
            for (Visit visit : pet.getVisits()) {
                em.createQuery("DELETE FROM Visit visit WHERE id = :visitId")
                    .setParameter("visitId", visit.getId())
                    .executeUpdate();
            }
            em.createQuery("DELETE FROM Pet pet WHERE id = :petId")
                .setParameter("petId", pet.getId())
                .executeUpdate();
        }
        em.createQuery("DELETE FROM PetType pettype WHERE id = :petTypeId")
            .setParameter("petTypeId", petTypeId)
            .executeUpdate();
    }

    /**
     * Deletes a specialty and detaches it from every vet that referenced it.
     */
    public static void deleteSpecialty(EntityManager em, Specialty specialty) {
        em.remove(em.contains(specialty) ? specialty : em.merge(specialty));
        Integer specialtyId = specialty.getId();
        em.createNativeQuery("DELETE FROM vet_specialties WHERE specialty_id = :specialtyId")
            .setParameter("specialtyId", specialtyId)
            .executeUpdate();
        em.createQuery("DELETE FROM Specialty specialty WHERE id = :specialtyId")
            .setParameter("specialtyId", specialtyId)
            .executeUpdate();
    }

    /**
     * Deletes a visit. A visit owns no other rows, so nothing cascades from here.
     */
    public static void deleteVisit(EntityManager em, Visit visit) {
        em.remove(em.contains(visit) ? visit : em.merge(visit));
    }

}
