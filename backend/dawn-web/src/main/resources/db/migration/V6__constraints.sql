ALTER TABLE seat_instance
    ADD CONSTRAINT fk_seat_instance_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservation(id) ON DELETE SET NULL;
